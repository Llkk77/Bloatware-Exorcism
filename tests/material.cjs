const {chromium}=require('playwright');
const axe=require('axe-core');
const path=require('path');
const assert=require('node:assert/strict');
(async()=>{
const browser=await chromium.launch({headless:true,executablePath:process.env.EXORCISM_CHROME,args:['--no-sandbox']});
const page=await browser.newPage({viewport:{width:412,height:915}});
const errors=[],remote=[];
page.on('pageerror',e=>errors.push(e.message));
page.on('request',r=>{if(/^https?:/.test(r.url()))remote.push(r.url())});
await page.goto('file://'+path.resolve(__dirname,'../app/src/main/assets/index.html'));
await page.waitForSelector('.hero');await page.evaluate(()=>document.fonts.ready);
await page.addScriptTag({content:axe.source});
for(const name of ['Home','Packages','Backups','Restore','Advanced']){
 await page.locator('nav').getByRole('button',{name,exact:true}).click();
 const result=await page.evaluate(()=>axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}}));
 assert.deepEqual(result.violations.map(v=>({id:v.id,nodes:v.nodes.map(n=>n.target)})),[],name+' accessibility');
}
for(const width of [320,360,412,600,1000,1440]){
 await page.setViewportSize({width,height:915});
 for(const name of ['Home','Packages','Backups','Restore','Advanced']){
  await page.locator('nav').getByRole('button',{name,exact:true}).click();
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,name+' overflow '+width);
 }
 await page.locator('nav').getByRole('button',{name:'Packages',exact:true}).click();
 await page.locator('.app-row').getByRole('checkbox').first().check();await page.locator('.selection').waitFor();
 const selection=await page.locator('#selection').boundingBox();const nav=await page.locator('nav').boundingBox();
 if(width<600)assert.ok(selection.y+selection.height<=nav.y,'Bottom navigation overlap '+width);
 else assert.ok(selection.x>=nav.x+nav.width,'Navigation rail overlap '+width);
}
await page.setViewportSize({width:412,height:915});
await page.locator('nav').getByRole('button',{name:'Packages',exact:true}).click();
await page.getByRole('textbox',{name:'Search packages',exact:true}).fill('Analytics');
assert.equal(await page.locator('.app-row').count(),1);
await page.getByRole('textbox',{name:'Search packages',exact:true}).fill('');
await page.evaluate(()=>document.documentElement.style.fontSize='32px');
assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,'200% text overflow');
await page.evaluate(()=>document.documentElement.style.fontSize='');
const palettes=await page.evaluate(()=>{
 const results=[];
 for(const seed of ['#4563eb','#008577','#c65320']){
  window.applyDeviceTheme({seed,dynamic:true});results.push({...window.materialColors});
 }
 window.applyDeviceTheme();return results;
});
assert.equal(new Set(palettes.map(p=>p.primary)).size,3,'Dynamic seed did not change palette');
function luminance(hex){return hex.slice(1).match(/../g).map(n=>parseInt(n,16)/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4).reduce((n,x,i)=>n+x*[.2126,.7152,.0722][i],0)}
for(const p of palettes)for(const [bg,fg] of [['surface','on-surface'],['primary','on-primary'],['primary-container','on-primary-container'],['secondary-container','on-secondary-container'],['tertiary-container','on-tertiary-container'],['surface-container-high','on-surface-variant']]){
 const l=[luminance(p[bg]),luminance(p[fg])].sort((a,b)=>b-a);assert.ok((l[0]+.05)/(l[1]+.05)>=4.5,bg+' text contrast');
}
await page.emulateMedia({reducedMotion:'reduce'});
await page.getByRole('button',{name:'About the app',exact:true}).click();await page.getByRole('dialog').waitFor();
await page.keyboard.press('Shift+Tab');
assert.ok(await page.evaluate(()=>{let a=document.activeElement;while(a?.shadowRoot?.activeElement)a=a.shadowRoot.activeElement;return a?.getRootNode().host?.closest('md-dialog')||a?.closest('md-dialog')||document.activeElement?.closest('md-dialog')}),'Dialog focus escaped');
await page.keyboard.press('Escape');await page.getByRole('dialog').waitFor({state:'hidden'});
assert.equal(await page.evaluate(()=>document.activeElement.id),'about-icon','Dialog focus not restored');
assert.deepEqual(errors,[]);assert.deepEqual(remote,[]);
console.log('Material UI: WCAG A/AA automated scan, six responsive widths, selection placement, search, 200% text, three dynamic palettes/contrast, keyboard dialog and offline loading passed');
await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
